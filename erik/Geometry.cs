using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text;

namespace goro
{
    internal static class Geometry
    {
        public static double Clamp(double val, double low, double high)
        {
            if (double.IsNaN(val))
            {
                return low;
            }
            return Math.Min(Math.Max(val, low), high);
        }

        public static double DegreesToRadians(double degrees)
        {
            return (((degrees * 2.0) * 3.1415926535897931) / 360.0);
        }

        public static double Distance(PointF a, PointF b)
        {
            return Math.Sqrt(Math.Pow((double)(a.X - b.X), 2.0) + Math.Pow((double)(a.Y - b.Y), 2.0));
        }

        public static PointF GetCenter(this RectangleF bounds)
        {
            return MakePoint((double)((bounds.Right - bounds.Left) / 2f), (double)((bounds.Bottom - bounds.Top) / 2f));
        }


        public static PointF MakePoint(double x, double y)
        {
            return new PointF((float)x, (float)y);
        }

        public static PointF MidPointWith(this PointF a, PointF b)
        {
            return MakePoint((double)((a.X + b.X) / 2f), (double)((a.Y + b.Y) / 2f));
        }

        public static double NormalizeBearing(double a)
        {
            while (a > 180.0)
            {
                a -= 360.0;
            }
            while (a < -180.0)
            {
                a += 360.0;
            }
            return a;
        }

        public static double NormalizeHeading(double a)
        {
            while (a > 360.0)
            {
                a -= 360.0;
            }
            while (a < 0.0)
            {
                a += 360.0;
            }
            return a;
        }

        public static double RadiansToDegrees(double radians)
        {
            return (((radians / 2.0) / 3.1415926535897931) * 360.0);
        }

        public static double Heading(this PointF point, PointF target)
        {
            return Geometry.RadiansToDegrees(HeadingRadians(point, target));
        }

        public static double HeadingRadians(this PointF point, PointF target)
        {
            return Math.Atan2(target.X - point.X, target.Y - point.Y);
        }

        public static PointF VectorProjection(this PointF point, double heading, double distance)
        { 
            double a = DegreesToRadians(heading);
            return MakePoint(point.X + (distance * Math.Sin(a)), point.Y + (distance * Math.Cos(a)));
        }

        public static PointF ShiftBy(this PointF point, double heading, double distance, RectangleF bounds)
        {
            double a = DegreesToRadians(heading);
            double x = Clamp(point.X + (distance * Math.Sin(a)), (double)bounds.Left, (double)bounds.Right);
            double y = Clamp(point.Y + (distance * Math.Cos(a)), (double)bounds.Top, (double)bounds.Bottom);
            return MakePoint(x, y);
        }
    }
}
