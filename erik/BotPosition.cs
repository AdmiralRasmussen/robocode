using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using Robocode;

namespace goro
{
    internal class BotPosition
    {
        public enum Motion
        {
            Reverse = 1,
            Coast = 2,
            Accelerate = 3
        }

        public enum Turn
        {
            TurnLeft = 1,
            Straight = 2,
            TurnRight = 3
        }

        public PointF Location;
        public double Heading;
        public double Velocity;

        public BotPosition(PointF location, double heading, double velocity)
        {
            Location = location;
            Heading = heading;
            Velocity = velocity;
        }

        public BotPosition(double x, double y, double heading, double velocity)
        {
            Location = Geometry.MakePoint(x, y);
            Heading = heading;
            Velocity = velocity;
        }

        public static BotPosition[][] AllMoves(BotPosition bot)
        {
            return BotPosition.AllMoves(bot, 1);
        }

        public static BotPosition[][] AllMoves(BotPosition bot, int turns)
        {
            List<BotPosition[]> moves = new List<BotPosition[]>();
            moves.Add(BotPosition.Move(bot, Motion.Accelerate, Turn.TurnLeft, turns));
            moves.Add(BotPosition.Move(bot, Motion.Accelerate, Turn.Straight, turns));
            moves.Add(BotPosition.Move(bot, Motion.Accelerate, Turn.TurnRight, turns));
            //moves.Add(BotPosition.Move(bot, Motion.Coast, Turn.TurnLeft, turns));
            //moves.Add(BotPosition.Move(bot, Motion.Coast, Turn.Straight, turns));
            //moves.Add(BotPosition.Move(bot, Motion.Coast, Turn.TurnRight, turns));
            moves.Add(BotPosition.Move(bot, Motion.Reverse, Turn.TurnLeft, turns));
            moves.Add(BotPosition.Move(bot, Motion.Reverse, Turn.Straight, turns));
            moves.Add(BotPosition.Move(bot, Motion.Reverse, Turn.TurnRight, turns));
            return moves.ToArray();
        }

        public static BotPosition[] Move(BotPosition bot, Motion motion, Turn turn)
        {
            return BotPosition.Move(bot, motion, turn, 1);
        }

        public static BotPosition[] Move(BotPosition bot, Motion motion, Turn turn, int turns)
        {
            List<BotPosition> moves = new List<BotPosition>(turns);

            if (turns > 1)
            {
                var arr = BotPosition.Move(bot, motion, turn, turns - 1);
                moves.AddRange(arr);
                bot = arr[turns - 2];
            }
            double velocity = bot.Velocity;
            double heading = bot.Heading;

            switch (motion)
            {
                case Motion.Reverse:
                    if (velocity >= Rules.DECELERATION)
                    {
                        velocity = velocity - Rules.DECELERATION;
                    }
                    else if (velocity > 0)
                    {
                        velocity = 0;
                    }
                    else if (velocity * -1 + Rules.ACCELERATION >= Rules.MAX_VELOCITY)
                    {
                        velocity = -1 * Rules.MAX_VELOCITY;
                    }
                    else
                    {
                        velocity = velocity - Rules.ACCELERATION;
                    }
                    break;
                case Motion.Accelerate:
                    if (velocity + Rules.ACCELERATION >= Rules.MAX_VELOCITY)
                    {
                        velocity = Rules.MAX_VELOCITY;
                    }
                    else
                    {
                        velocity = velocity + Rules.ACCELERATION;
                    }
                    break;
            }
            switch (turn)
            {
                case Turn.TurnLeft:
                    if (motion == Motion.Reverse)
                    {
                        heading = heading + Rules.GetTurnRate(Math.Abs(velocity));
                    }
                    else
                    {
                        heading = heading - Rules.GetTurnRate(Math.Abs(velocity));
                    }
                    break;
                case Turn.TurnRight:
                    if (motion == Motion.Reverse)
                    {
                        heading = heading - Rules.GetTurnRate(Math.Abs(velocity));
                    }
                    else
                    {
                        heading = heading + Rules.GetTurnRate(Math.Abs(velocity));
                    }
                    break;
            }
            moves.Add(new BotPosition(bot.Location.VectorProjection(heading, velocity), heading, velocity));
            return moves.ToArray();
        }
    }
}
