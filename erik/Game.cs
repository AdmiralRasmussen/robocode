using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Robocode;

namespace goro
{
    internal static class Game
    {
        public static double GetBulletSpeed(double power)
        {
            return Rules.GetBulletSpeed(power);
            //return (20.0 - (3.0 * power));
        }

        public static double GetDamage(this Bullet bullet)
        {
            return Rules.GetBulletDamage(bullet.Power);
            //double num = bullet.Power * 4.0;
            //if (bullet.Power > 1.0)
            //{
            //    num += (bullet.Power - 1.0) * 2.0;
            //}
            //return num;
        }

        public static double GetMaxTurnRate(double velocity)
        {
            return Rules.GetTurnRate(velocity);
            //return 10 - .75 * velocity;
        }

        public static double GetMaxVelocity(double radius)
        {
            // For small angles, sin(x) = x (note: x in radians)
            // radius = velocity * 180 / (10 - .75 * velocity) * PI

            // Solving for velocity...
            // velocity = 10*PI*radius / (180 + .75*PI*radius)

            return 10 * Math.PI * radius / (180 + .75 * Math.PI * radius);
        }

        public static double GetMinRadius(double velocity)
        {
            // degrees per turn DPT = 10 - .75 * Velocity
            // radius = Velocity / Sin(DPT)

            // Velocity  DPT   Sin(DPT)     Radius
            // 0         10    .173648178   0 
            // 1         9.25  .160742566   6.221127529
            // 2         8.50  .147809411   13.53093815
            // 3         7.75  .13485093    22.24678757
            // 4         7.00  .121869343   32.82203619
            // 5         6.25  .108866875   45.92765253
            // 6         5.50  .095845753   62.60058315
            // 7         4.75  .082808208   84.53268354
            // 8         4.00  .069756474   114.6846962

            return velocity / Math.Sin(10 - .75 * velocity);
        }
    }
}
