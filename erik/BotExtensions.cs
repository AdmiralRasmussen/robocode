using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using Robocode;

namespace goro
{
    internal static class BotExtensions
    {
        public static double DistanceTo(this Robot bot, PointF dest)
        {
            return Geometry.Distance(bot.GetLocation(), dest);
        }

        public static double GetDistanceTo(this Robot bot, PointF target)
        {
            return Geometry.Distance(bot.GetLocation(), target);
        }

        public static double GetHeadingTo(this Robot bot, PointF target)
        {
            return Geometry.RadiansToDegrees(Math.Atan2(target.X - bot.X, target.Y - bot.Y));
        }

        public static PointF GetLocation(this Robot bot)
        {
            return Geometry.MakePoint(bot.X, bot.Y);
        }

        public static RectangleF GetArenaBounds(this Robot bot)
        {
            return new RectangleF(((float)bot.Width) / 2f, ((float)bot.Height) / 2f, (float)(bot.BattleFieldWidth - bot.Width), (float)(bot.BattleFieldHeight - bot.Height));
        }

        public static bool TurnGunTo(this Robot bot, double heading, [Optional, DefaultParameterValue(0.0)] double headingThreshold)
        {
            double num = Geometry.NormalizeHeading(heading - bot.GunHeading);
            if (Math.Abs(num) > headingThreshold)
            {
                bot.TurnGunRight(num);
                return !(num == 0.0);
            }
            return false;
        }

        public static bool TurnGunTo(this Robot bot, PointF target, [Optional, DefaultParameterValue(0.0)] double headingThreshold)
        {
            return bot.TurnGunTo(bot.GetHeadingTo(target), headingThreshold);
        }

        public static bool TurnRadarTo(this Robot bot, double heading)
        {
            double degrees = Geometry.NormalizeHeading(heading - bot.RadarHeading);
            bot.TurnRadarRight(degrees);
            return !(degrees == 0.0);
        }

        public static bool TurnRadarTo(this Robot bot, PointF target)
        {
            return bot.TurnRadarTo(bot.GetHeadingTo(target));
        }

        public static bool TurnTo(this Robot bot, double heading)
        {
            double degrees = Geometry.NormalizeHeading(heading - bot.Heading);
            bot.TurnRight(degrees);
            return !(degrees == 0.0);
        }
    }
}
