using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using System.Text;
using Robocode;
using Robocode.Util;

namespace goro
{
    /// <summary>
    ///   Squirrely - a wiggly robot by Erik
    /// </summary>
    public class Squirrely : AdvancedRobot
    {
        private List<IDoIt> TurnList = new List<IDoIt>() { new Turn { Radius = 15, Angle = 120 } 
                                               , new Turn { Radius = 15, Angle = -120 }
                                               , new Turn { Radius = 45, Angle = 120 } 
                                               , new Turn { Radius = 45, Angle = -120 }
                                               , new Turn { Radius = 90, Angle = 120 } 
                                               , new Turn { Radius = 90, Angle = -120 }
                                               , new Turn { Radius = 45, Angle = 120 } 
                                               , new Turn { Radius = 45, Angle = -120 }
                                               , new Turn { Radius = 15, Angle = 120 } 
                                               , new Turn { Radius = 15, Angle = -120 }
                                               , new Turn { Radius = 0, Angle = 180 }
        };

        private int CurrentInstruction = 0;

        private BotState Bot;
        private bool RadarClockwise = true;

        public override void Run()
        {
            try
            {
                // Set colors
                SetColors(Color.Orange, Color.Yellow, Color.Black, Color.Yellow, Color.Yellow);
                IsAdjustRadarForGunTurn = true;
                //IsAdjustRadarForRobotTurn = true; // Automatic if IsAdjustRadarForGunTurn = true.
                IsAdjustGunForRobotTurn = true;

                SetTurnRadarRight(10000);

                // Loop forever
                while (true)
                {
                    if (TurnList[CurrentInstruction].DoIt(this) == false)
                    {
                        if (CurrentInstruction < TurnList.Count - 1)
                        {
                            CurrentInstruction++;
                        }
                        else
                        {
                            CurrentInstruction = 0;
                        }
                    }
                    Out.WriteLine("Time: {0}", Time);
                    SetFire(.1);
                    Execute();
                }
            }
            catch (Exception ex) 
            {
                Out.WriteLine(ex.ToString());
                throw;
            }
        }

        public override void OnBulletHit(BulletHitEvent e)
        {
            //    ITargetPredictor predictor = this.BulletStrategies[evnt.Bullet];
            //    PredictorStats local1 = this.Predictors[predictor];
            //    local1.Hits++;
            //    this.ShotsHit++;
            base.OnBulletHit(e);
        }

        public override void OnBulletHitBullet(BulletHitBulletEvent e)
        {
            base.OnBulletHitBullet(e);
        }

        public override void OnBulletMissed(BulletMissedEvent e)
        {
            //    ITargetPredictor predictor = this.BulletStrategies[evnt.Bullet];
            //    PredictorStats local1 = this.Predictors[predictor];
            //    local1.Misses++;
            base.OnBulletMissed(e);
        }

        /// 
        ///<summary>
        ///  onHitRobot:  If it's our fault, we'll stop turning and moving,
        ///  so we need to turn again to keep spinning.
        ///</summary>
        public override void OnHitRobot(HitRobotEvent e)
        {
            if (e.Bearing > -10 && e.Bearing < 10)
            {
                Fire(3);
            }
            if (e.IsMyFault)
            {
                //VelocityRate = (-1 * VelocityRate);
                //SetTurnRight(10);
            }
        }

        public override void OnHitWall(HitWallEvent e)
        {
            // Move away from the wall
            //VelocityRate = (-1 * VelocityRate);
            //SetTurnRight(30);
        }

        /// <summary>
        ///   onScannedRobot: Fire hard!
        /// </summary>
        public override void OnScannedRobot(ScannedRobotEvent e)
        {
            Bot = new BotState(this, e);
            double gunBearing = Utils.NormalRelativeAngleDegrees(Heading + e.Bearing - GunHeading);
            double radarBearing = Utils.NormalRelativeAngleDegrees(Heading + e.Bearing - RadarHeading);
            //Out.WriteLine("ScanRobot - Time: {6} Heading: {0} Bearing: {1} GunHeading {2} GunBearing {3} RadarHeading {4} RadarBearing {5}", Heading, e.Bearing, GunHeading, gunBearing, RadarHeading, radarBearing, Time);
            if (radarBearing > 0)
            {
                RadarClockwise = true;
            }
            else
            {
                RadarClockwise = false;
            }
            SetTurnRadarRight(radarBearing*2);
            SetTurnGunRight(gunBearing);
            //if (gunBearing > -5 && gunBearing < 5)
            //{
            //    Fire(3);
            //}
        }

        public override void OnSkippedTurn(SkippedTurnEvent evnt)
        {
            Out.WriteLine("Skipped - Time: {0}, SkippedTime: {1} SkippedTurn: {2} Priority: {3}", Time, evnt.SkippedTurn, evnt.Time, evnt.Priority);
            base.OnSkippedTurn(evnt);
        }

        /// <summary>
        ///   Paint a red circle around our PaintingRobot
        /// </summary>
        public override void OnPaint(IGraphics graphics)
        {
            //double velocity = Velocity;
            //double heading = Heading;
            //if (velocity < 0)
            //{
            //    velocity = velocity * -1;
            //    heading = Utils.NormalAbsoluteAngleDegrees(heading + 180);
            //}
            //Paint.BotCircle(graphics, X, Y, true);
            //Paint.Forecast(graphics, X, Y, Heading, Velocity, 20);
            //Paint.BotVelocityVector(graphics, X, Y, HeadingRadians, Velocity);

            if (Bot != null)
            {
                double distance = this.GetDistanceTo(Bot.Location);
                int turns = (int)(distance/Rules.GetBulletSpeed(Rules.MIN_BULLET_POWER));
                Out.WriteLine("Distance {0}, Turns {1}", distance, turns);
                Paint.BotCircle(graphics, Bot.Location.X, Bot.Location.Y, false);
                Paint.BotForecast(graphics, Bot.Location.X, Bot.Location.Y, Bot.Heading, Bot.Velocity, turns);
                Paint.BotVelocityVector(graphics, Bot.Location.X, Bot.Location.Y, Bot.HeadingRadians, Bot.Velocity);
            }
        }
    }

}