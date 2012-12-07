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
    ///   HogPile - a ramming robot by Erik
    /// </summary>
    public class HogPile : AdvancedRobot
    {
        private BotState Bot;
        private PointF ProjectedLocation;
        private bool RadarClockwise = true;

        public override void Run()
        {
            try
            {
                // Set colors
                SetColors(Color.Blue, Color.Gray, Color.White, Color.White, Color.White);
                IsAdjustRadarForGunTurn = true;
                //IsAdjustRadarForRobotTurn = true; // Automatic if IsAdjustRadarForGunTurn = true.
                //IsAdjustGunForRobotTurn = true;

                SetTurnRadarRight(10000);

                // Loop forever
                while (true)
                {
                    if (RadarTurnRemaining == 0)
                    {
                        if (RadarClockwise)
                        {
                            SetTurnRadarRight(10000);
                        }
                        else
                        {
                            SetTurnRadarLeft(10000);
                        }
                    }
                    if (Bot != null)
                    {
                        this.DebugProperty["Distance"] = this.DistanceTo(Bot.Location).ToString();
                    }
                    if (this.GunHeat == 0 && Time - Bot.Turn < 5 && Bot != null && this.DistanceTo(Bot.Location) < 150)
                    {
                        SetFire(2);
                    }
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
            if (e.Name.IndexOf("HogPile") < 0)
            {
                if (e.Bearing > -10 && e.Bearing < 10)
                {
                    Fire(2);
                }
                if (e.IsMyFault)
                {
                    //VelocityRate = (-1 * VelocityRate);
                    //SetTurnRight(10);
                }
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
            if (e.Name.IndexOf("HogPile") < 0)
            {
                Bot = new BotState(this, e);
   
                int turns = (int)(e.Distance/Rules.GetBulletSpeed(Rules.MAX_BULLET_POWER));
                ProjectedLocation = Bot.GetProjectedLocation(turns, 2.0);
                double projHeading = Geometry.MakePoint(X, Y).Heading(ProjectedLocation);
                double gunBearing = Utils.NormalRelativeAngleDegrees(projHeading - GunHeading);
                //double gunBearing = Utils.NormalRelativeAngleDegrees(Heading + e.Bearing - GunHeading);
                double radarBearing = Utils.NormalRelativeAngleDegrees(Heading + e.Bearing - RadarHeading);
                //Out.WriteLine("Distance: {0}, Turn: {1}, Projected: {2},{3}, Heading: {4}, GunBearing: {5}", e.Distance, turns, ProjectedLocation.X, ProjectedLocation.Y, projHeading, gunBearing);
                if (radarBearing > 0)
                {
                    RadarClockwise = true;
                }
                else
                {
                    RadarClockwise = false;
                }
                SetTurnRadarRight(radarBearing * 2);
                SetTurnGunRight(gunBearing);
                SetTurnRight(e.Bearing);
                SetAhead(e.Distance);
                //if (distance > 100) ahead(distance/3);
                // if (distance < 80) back(20);
            }
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
                int turns = (int)(distance/Rules.GetBulletSpeed((Rules.MAX_BULLET_POWER - Rules.MIN_BULLET_POWER)/2));
                //Out.WriteLine("Distance {0}, Turns {1}", distance, turns);
                //Paint.BotCircle(graphics, Bot.Location.X, Bot.Location.Y, false);
                Paint.BotBox(graphics, Pens.Yellow, ProjectedLocation.X, ProjectedLocation.Y, Bot.Heading);
                Paint.BotForecast(graphics, Bot.Location.X, Bot.Location.Y, Bot.Heading, Bot.Velocity, turns);
                Paint.BotVelocityVector(graphics, Bot.Location.X, Bot.Location.Y, Bot.HeadingRadians, Bot.Velocity);
            }
        }
    }

}