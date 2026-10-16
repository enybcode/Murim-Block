package com.colleuhu95.bloodsect.ninja;

/** Grounded Blood Cloud Step: draw stance, committed horizontal cut, held finish, recovery. */
public final class DaoAttackMotion {
    public static final int CLOUD_STEP=0, M1=1;
    public static final int WINDUP=10, DASH_END=14, DURATION=28;
    public record Pose(float armX, float armY, float armZ, float elbowX, float torsoY, float guardX, float guardElbowX, float tassel, float torsoX, float crouch, float rightThigh, float rightKnee, float leftThigh, float leftKnee, float guardZ, float wristX, float wristY, float wristZ) {}
    public record Trail(float start,float end,float life,float heel) {}
    private static final Trail TRAIL=new Trail(10,14,6,8);
    private static final float[] TIMES={0F,3.3F,8.3F,10F,11.2F,13.5F,18.5F,21F,28F};
    private static final float[][] POSES={
        {0.00000F,0.00000F,5.00000F,0.00000F,0.00000F,0.00000F,0.00000F,0.00000F,0.00000F,0.00000F,0.00000F,2.00000F,0.00000F,2.00000F,-5.00000F,0.00000F,0.00000F,0.00000F},
        {-22.00000F,18.00000F,-55.00000F,-82.00000F,28.00000F,-28.00000F,-75.00000F,-12.00000F,28.00000F,1.60000F,-30.00000F,60.00000F,-25.00000F,50.00000F,-12.00000F,-123.50421F,10.47755F,124.22055F},
        {-24.00000F,20.00000F,-57.00000F,-80.00000F,30.00000F,-30.00000F,-75.00000F,-6.00000F,30.00000F,1.80000F,-32.00000F,64.00000F,-27.00000F,54.00000F,-12.00000F,-120.92913F,5.62999F,123.92831F},
        {-30.00000F,22.00000F,-52.00000F,-65.00000F,25.00000F,-38.00000F,-65.00000F,-22.00000F,40.00000F,2.40000F,-40.00000F,72.00000F,-24.00000F,55.00000F,-18.00000F,-108.98479F,11.39627F,110.95282F},
        {-65.00000F,8.00000F,-12.00000F,-18.00000F,4.00000F,-48.00000F,-40.00000F,42.00000F,35.00000F,2.40000F,-36.00000F,72.00000F,-34.00000F,68.00000F,-20.00000F,-263.73659F,-35.49298F,-57.09080F},
        {-78.00000F,-25.00000F,68.00000F,-6.00000F,-42.00000F,-45.00000F,-55.00000F,62.00000F,25.00000F,2.00000F,-35.00000F,70.00000F,-29.00000F,58.00000F,-30.00000F,-348.00411F,53.41663F,-156.47877F},
        {-78.00000F,-25.00000F,68.00000F,-6.00000F,-42.00000F,-45.00000F,-55.00000F,5.00000F,22.00000F,2.00000F,-35.00000F,70.00000F,-29.00000F,58.00000F,-30.00000F,-350.26677F,50.95898F,-157.24809F},
        {-48.00000F,-22.00000F,62.00000F,-22.00000F,-28.00000F,-28.00000F,-35.00000F,-18.00000F,14.00000F,1.30000F,-28.00000F,56.00000F,-24.00000F,48.00000F,-18.00000F,-356.93555F,40.83597F,-201.24852F},
        {0.00000F,0.00000F,5.00000F,0.00000F,0.00000F,0.00000F,0.00000F,0.00000F,0.00000F,0.00000F,0.00000F,2.00000F,0.00000F,2.00000F,-5.00000F,0.00000F,0.00000F,0.00000F}
    };
    public static int duration(int variant) {return variant==M1 ? DaoM1Motion.DURATION : DURATION;}
    public static int trailCount(int variant) {return 1;}
    public static Trail trail(int variant,int index) {return variant==M1 ? DaoM1Motion.TRAIL : TRAIL;}
    public static float pulse(float age,int variant) {
        if(variant==M1) return age>=1.5F && age<=10 ? .35F*(float)Math.sin(Math.PI*(age-1.5F)/8.5F) : 0;
        if(age<2 || age>DASH_END || !Float.isFinite(age)) return 0;
        return Math.min(1,Math.min((age-2)/4,(DASH_END-age)/2))*(.45F+.25F*(float)Math.pow(Math.sin(age*.7F),4));
    }
    public static Pose sample(float age,int variant) {
        if(variant==M1) return DaoM1Motion.sample(age);
        if(!Float.isFinite(age)) age=0;
        age=Math.max(0,Math.min(DURATION,age));
        int i=0;while(i<TIMES.length-2 && age>TIMES[i+1]) i++;
        float u=(age-TIMES[i])/(TIMES[i+1]-TIMES[i]);u=u*u*(3-2*u);
        var a=POSES[i];var b=POSES[i+1];
        return new Pose(lerp(a,b,0,u),lerp(a,b,1,u),lerp(a,b,2,u),lerp(a,b,3,u),lerp(a,b,4,u),lerp(a,b,5,u),lerp(a,b,6,u),lerp(a,b,7,u),lerp(a,b,8,u),lerp(a,b,9,u),lerp(a,b,10,u),lerp(a,b,11,u),lerp(a,b,12,u),lerp(a,b,13,u),lerp(a,b,14,u),lerp(a,b,15,u),lerp(a,b,16,u),lerp(a,b,17,u));
    }
    private static float lerp(float[] a,float[] b,int i,float u) {return a[i]+(b[i]-a[i])*u;}
    private DaoAttackMotion() {}
}
