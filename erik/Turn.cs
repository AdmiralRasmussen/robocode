using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Robocode;
using Robocode.Util;

namespace goro
{
    internal class Turn : IDoIt
    {
        public double Radius { get; set; }
        public double Angle { get; set; }
        private double m_maxVelocity;
        private double m_stopAngle;
        private bool isSet = false;
        public bool DoIt(AdvancedRobot robot)
        {
            if (!isSet)
            {
                isSet = true;
                m_stopAngle = Utils.NormalAbsoluteAngleDegrees(robot.Heading + Angle);
                m_maxVelocity = Game.GetMaxVelocity(Radius);
                robot.MaxVelocity = m_maxVelocity;
                //robot.MaxTurnRate = 
                robot.SetTurnRight(Angle);
                robot.SetAhead(1000);
            }
            if (robot.TurnRemaining == 0)
            {
                isSet = false;
                return false;
            }
            //robot.Out.WriteLine("Radius: {0}, Angle: {1}, Velocity: {2}, TurnRemaining: {3}", Radius, Angle, m_maxVelocity, robot.TurnRemaining);
            return true;
        }


    }
}
