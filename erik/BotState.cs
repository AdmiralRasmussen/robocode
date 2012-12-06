using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using Robocode;
using Robocode.Util;

namespace goro
{
    internal class BotState
    {
        public Robot Observer;
        public PointF Location;
        public double Heading;
        public double Velocity;
        public long Turn;
        public double Energy;
        public BotState(Robot bot, ScannedRobotEvent evnt)
        {
            double heading = bot.Heading + evnt.Bearing;
            this.Observer = bot;
            this.Energy = evnt.Energy;
            this.Location = bot.GetLocation().ShiftBy(heading, evnt.Distance, bot.GetArenaBounds());
            if (evnt.Velocity < 0)
            {
                this.Velocity = evnt.Velocity * -1;
                this.Heading = Utils.NormalAbsoluteAngleDegrees(evnt.Heading + 180);
            }
            else
            {
                this.Heading = evnt.Heading;
                this.Velocity = evnt.Velocity;
            }
            this.Turn = bot.Time;
        }

        public double HeadingRadians
        {
            get
            {
                return Geometry.DegreesToRadians(this.Heading);
            }
        }
        public PointF GetProjectedLocation([Optional, DefaultParameterValue(0.0)] double turnsInFuture, [Optional, DefaultParameterValue(0.0)] double velocityOverride)
        {
            double num = this.Observer.Time - this.Turn;
            double distance = (num + turnsInFuture) * velocityOverride;
            return this.Location.ShiftBy(this.Heading, distance, this.Observer.GetArenaBounds());
        }

        public long Age
        {
            get
            {
                return (this.Observer.Time - this.Turn);
            }
        }
    }
}
