using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text;
using Robocode;
using Robocode.Util;

namespace goro
{
    internal class Straight : IDoIt
    {
        public double Distance { get; set; }
        private PointF m_startPoint;
        private bool isSet = false;
        public bool DoIt(AdvancedRobot robot)
        {
            if (!isSet)
            {
                isSet = true;
                m_startPoint = Geometry.MakePoint(robot.X, robot.Y);
                robot.SetAhead(10000);
            }
            if (robot.GetDistanceTo(m_startPoint) > Distance)
            {
                isSet = false;
                return false;
            }
            //robot.Out.WriteLine("Radius: {0}, Angle: {1}, Velocity: {2}, TurnRemaining: {3}", Radius, Angle, m_maxVelocity, robot.TurnRemaining);
            return true;
        }


    }
}
